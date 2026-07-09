package com.villagesat.compliance.domain.port.out;

import com.villagesat.compliance.domain.model.Screening;

public interface ScreeningRepository {

    Screening save(Screening screening);
}
