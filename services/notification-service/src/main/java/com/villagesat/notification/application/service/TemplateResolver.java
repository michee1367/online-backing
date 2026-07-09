package com.villagesat.notification.application.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TemplateResolver {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    private static final Map<String, String> TEMPLATES = Map.of(
            "txn.completed.sender", "Transfert de {{amount}} {{currency}} envoyé à {{destination}}. Frais: {{fee}}. Nouveau solde disponible.",
            "txn.completed.receiver", "Vous avez reçu {{amount}} {{currency}} de {{sender}}.",
            "kyc.approved", "Félicitations ! Votre vérification KYC niveau {{level}} a été approuvée. Vos plafonds ont été mis à jour.",
            "kyc.rejected", "Votre demande KYC a été rejetée. Motif : {{reason}}. Veuillez soumettre à nouveau.",
            "wallet.created", "Bienvenue sur VillageSat ! Votre wallet {{currency}} a été créé. Numéro : {{accountNumber}}.",
            "otp.sms", "VillageSat - Votre code de vérification : {{code}}. Valable 5 minutes."
    );

    public String resolve(String templateCode, Map<String, String> variables) {
        String template = TEMPLATES.get(templateCode);
        if (template == null) {
            return null;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = variables.getOrDefault(key, "{{" + key + "}}");
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
