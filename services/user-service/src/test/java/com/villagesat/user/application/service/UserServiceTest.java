package com.villagesat.user.application.service;

import com.villagesat.user.domain.model.*;
import com.villagesat.user.domain.port.in.UserUseCase;
import com.villagesat.user.domain.port.out.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.villagesat.user.support.UserTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserProfileRepository profileRepository;

    @Mock
    KeycloakSyncPort keycloakSync;

    @Mock
    DataExportRepository dataExportRepository;

    @InjectMocks
    UserService userService;

    // ── provisionFromRegistration ──────────────────────────────────

    @Test
    void provisionFromRegistration_createsUserAndProfile() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                USER_ID, "alice@example.com", "+243810000001",
                "Alice", "Mutombo", "CD", 0, "PENDING_VERIFICATION"
        ));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.id()).isEqualTo(USER_ID);
        assertThat(saved.email()).isEqualTo("alice@example.com");
        assertThat(saved.firstName()).isEqualTo("Alice");
        assertThat(saved.status()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);

        verify(profileRepository).save(any(UserProfile.class));
        verify(keycloakSync).syncUserProfile(eq(USER_ID), any(User.class), any(UserProfile.class));
        verify(keycloakSync).updateKycLevel(USER_ID, 0);
    }

    @Test
    void provisionFromRegistration_existingUser_skips() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);

        userService.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                USER_ID, "alice@example.com", null,
                "Alice", "Mutombo", "CD", 0, "ACTIVE"
        ));

        verify(userRepository, never()).save(any());
        verifyNoInteractions(keycloakSync);
    }

    @Test
    void provisionFromRegistration_invalidStatus_fallsToPendingVerification() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.provisionFromRegistration(new UserUseCase.ProvisionUserCommand(
                USER_ID, "alice@example.com", null,
                "Alice", "Mutombo", "CD", 0, "INVALID_STATUS"
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(User.UserStatus.PENDING_VERIFICATION);
    }

    // ── getCurrentUser ────────────────────────────────────────────

    @Test
    void getCurrentUser_returnsUserWithProfile() {
        User user = activeUser();
        UserProfile profile = defaultProfile();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        UserWithProfile result = userService.getCurrentUser(USER_ID);

        assertThat(result.user()).isEqualTo(user);
        assertThat(result.profile()).isEqualTo(profile);
    }

    @Test
    void getCurrentUser_noProfile_returnsDefault() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UserWithProfile result = userService.getCurrentUser(USER_ID);

        assertThat(result.profile().preferredLanguage()).isEqualTo("fr");
        assertThat(result.profile().timezone()).isEqualTo("Africa/Kinshasa");
    }

    @Test
    void getCurrentUser_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(USER_ID))
                .isInstanceOf(UserService.UserNotFoundException.class)
                .hasMessageContaining(USER_ID.toString());
    }

    // ── updateCurrentUser ─────────────────────────────────────────

    @Test
    void updateCurrentUser_updatesNamesAndProfile() {
        User user = activeUser();
        UserProfile profile = defaultProfile();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserUseCase.UpdateProfileCommand command = new UserUseCase.UpdateProfileCommand(
                "Bob", "Kabila", null,
                LocalDate.of(1990, 5, 15), "12 rue Lumumba", "Kinshasa", "CD",
                "fr", "Africa/Kinshasa", null, null
        );

        UserWithProfile result = userService.updateCurrentUser(USER_ID, command);

        assertThat(result.user().firstName()).isEqualTo("Bob");
        assertThat(result.user().lastName()).isEqualTo("Kabila");
        assertThat(result.profile().dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(result.profile().addressCity()).isEqualTo("Kinshasa");

        verify(keycloakSync).syncUserProfile(eq(USER_ID), any(User.class), any(UserProfile.class));
    }

    @Test
    void updateCurrentUser_updatesPhoneNumber() {
        User user = activeUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(defaultProfile()));
        when(profileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserUseCase.UpdateProfileCommand command = new UserUseCase.UpdateProfileCommand(
                null, null, "+243990000002",
                null, null, null, null,
                null, null, null, null
        );

        UserWithProfile result = userService.updateCurrentUser(USER_ID, command);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().phone()).isEqualTo("+243990000002");
    }

    @Test
    void updateCurrentUser_closedUser_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(closedUser()));

        UserUseCase.UpdateProfileCommand command = new UserUseCase.UpdateProfileCommand(
                "Bob", null, null,
                null, null, null, null,
                null, null, null, null
        );

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID, command))
                .isInstanceOf(UserService.UserException.class)
                .hasMessageContaining("pas actif");
    }

    @Test
    void updateCurrentUser_notFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        UserUseCase.UpdateProfileCommand command = new UserUseCase.UpdateProfileCommand(
                "Bob", null, null,
                null, null, null, null,
                null, null, null, null
        );

        assertThatThrownBy(() -> userService.updateCurrentUser(USER_ID, command))
                .isInstanceOf(UserService.UserNotFoundException.class);
    }

    // ── requestDataExport ─────────────────────────────────────────

    @Test
    void requestDataExport_createsExport() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(activeUser()));
        when(dataExportRepository.save(any(DataExportRequest.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DataExportRequest result = userService.requestDataExport(USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.status()).isEqualTo(DataExportRequest.ExportStatus.PENDING);
        verify(dataExportRepository).save(any(DataExportRequest.class));
    }

    @Test
    void requestDataExport_userNotFound_throws() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.requestDataExport(USER_ID))
                .isInstanceOf(UserService.UserNotFoundException.class);
    }
}
