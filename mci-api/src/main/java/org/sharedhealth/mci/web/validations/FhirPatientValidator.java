package org.sharedhealth.mci.web.validations;

import ca.uhn.fhir.model.dstu2.resource.Bundle;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ResultSeverityEnum;
import ca.uhn.fhir.validation.SingleValidationMessage;
import ca.uhn.fhir.validation.ValidationResult;
import org.apache.commons.io.IOUtils;
import org.hl7.fhir.instance.hapi.validation.FhirInstanceValidator;
import org.hl7.fhir.instance.model.StructureDefinition;
import org.sharedhealth.mci.web.config.MCIProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirContext;
import static org.sharedhealth.mci.web.util.FhirContextHelper.fhirHL7Context;

public class FhirPatientValidator {
    private final String PATIENT_PROFILE_FILE_PREFIX = "mcipatient";
    private List<Pattern> patientFieldErrors = new ArrayList<>();
    private volatile FhirValidator fhirValidator;
    private MCIProperties mciProperties;

    public FhirPatientValidator(MCIProperties mciProperties) {
        this.mciProperties = mciProperties;
        this.patientFieldErrors.add(Pattern.compile("/f:Patient/f:gender"));
        validatorInstance();
    }

    private static StructureDefinition loadProfileOrReturnNull(MCIProperties mciProperties, String profileName) {
        String profileText;
        try {
            String pathToProfile = mciProperties.getProfilesFolderPath() + profileName.toLowerCase() + ".profile.xml";
            profileText = IOUtils.toString(new FileInputStream(pathToProfile), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("No profile found for patient");
        }
        return fhirHL7Context.newXmlParser().parseResource(StructureDefinition.class,
                profileText);
    }

    public MCIValidationResult validate(Bundle bundle) {
        ValidationResult validationResult = fhirValidator.validateWithResult(bundle);
        changeWarningToErrorIfNeeded(validationResult);
        List<SingleValidationMessage> errorMessages = filterWarningsAndInformation(validationResult);
        return new MCIValidationResult(fhirContext, errorMessages);
    }

    private List<SingleValidationMessage> filterWarningsAndInformation(ValidationResult validationResult) {
        return validationResult.getMessages().stream().filter(
                singleValidationMessage -> singleValidationMessage.getSeverity().ordinal() > ResultSeverityEnum.WARNING.ordinal()
        ).collect(Collectors.toList());
    }

    private void changeWarningToErrorIfNeeded(ValidationResult validationResult) {
        validationResult.getMessages().forEach(validationMessage -> {
            if (isPossiblePatientFieldError(validationMessage.getLocationString())) {
                if (validationMessage.getSeverity().ordinal() <= ResultSeverityEnum.WARNING.ordinal()) {
                    validationMessage.setSeverity(ResultSeverityEnum.ERROR);
                }
            }
        });
    }

    private boolean isPossiblePatientFieldError(String locationString) {
        return patientFieldErrors.stream().anyMatch(pattern -> {
            Matcher matcher = pattern.matcher(locationString);
            return matcher.matches();
        });
    }

    private void validatorInstance() {
        if (fhirValidator == null) {
            fhirValidator = fhirContext.newValidator();
            FhirInstanceValidator validator = new FhirInstanceValidator();
            validator.setStructureDefintion(loadProfileOrReturnNull(mciProperties, PATIENT_PROFILE_FILE_PREFIX));
            fhirValidator.registerValidatorModule(validator);
        }
    }

}
