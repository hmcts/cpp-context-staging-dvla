package uk.gov.moj.cpp.stagingdvla.query.view.request;

import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class DriverSummaryQueryParameters {
    private String firstNames;
    private String lastName;
    private String dateOfBirth;
    private String postcode;
    private String caseId;
    private String gender;
    private Boolean exactFirstNamesMatch;
    private String reasonType;
    private String reference;

    public DriverSummaryQueryParameters() {
    }

    @SuppressWarnings("squid:S00107")
    public DriverSummaryQueryParameters(final String firstNames, final String lastName, final String dateOfBirth,
                                        final String postcode,
                                        final String gender,
                                        final String caseId,
                                        final Boolean exactFirstNamesMatch,
                                        final String reasonType,
                                        final String reference) {
        this.firstNames = firstNames;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.postcode = postcode;
        this.caseId = caseId;
        this.gender = gender;
        this.exactFirstNamesMatch = exactFirstNamesMatch;
        this.reasonType=reasonType;
        this.reference = reference;
    }

    public String getFirstNames() {
        return firstNames;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getGender() {
        return gender;
    }

    public Boolean isExactFirstNamesMatch() {
        return exactFirstNamesMatch;
    }

    public String getReasonType() {
        return reasonType;
    }

    public String getReference() {
        return reference;
    }

    public static class Builder {
        private String firstNames;
        private String lastName;
        private String dateOfBirth;
        private String postcode;
        private String caseId;
        private String gender;
        private Boolean exactFirstNamesMatch;

        private String reasonType;
        private String reference;

        public Builder firstNames(String firstNames) {
            this.firstNames = firstNames;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder dateOfBirth(String dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder postcode(String postcode) {
            this.postcode = postcode;
            return this;
        }

        public Builder gender(String gender) {
            this.gender = gender;
            return this;
        }

        public Builder exactFirstNamesMatch(Boolean exactFirstNamesMatch) {
            this.exactFirstNamesMatch = exactFirstNamesMatch;
            return this;
        }

        public Builder caseId(String caseId) {
            this.caseId = caseId;
            return this;
        }

        public Builder reasonType(String reasonType) {
            this.reasonType = reasonType;
            return this;
        }

        public Builder reference(String reference) {
            this.reference = reference;
            return this;
        }

        public DriverSummaryQueryParameters build() {
            return new DriverSummaryQueryParameters(firstNames, lastName, dateOfBirth, postcode, gender, caseId, exactFirstNamesMatch,reasonType, reference);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DriverSummaryQueryParameters{");
        sb.append("firstNames='").append(firstNames).append('\'');
        sb.append(", lastName='").append(lastName).append('\'');
        sb.append(", dateOfBirth='").append(dateOfBirth).append('\'');
        sb.append(", postcode='").append(postcode).append('\'');
        sb.append(", gender='").append(gender).append('\'');
        sb.append(", caseId='").append(caseId).append('\'');
        sb.append(", reasonType='").append(reasonType).append('\'');
        sb.append(", reference='").append(reference).append('\'');
        if(nonNull(exactFirstNamesMatch)) {
            sb.append(", exactFirstNamesMatch='").append(exactFirstNamesMatch.toString()).append('\'');
        }
        sb.append('}');
        return sb.toString();
    }

    public Map<String, Object> getParams() {
        final Map<String, Object> params = new HashMap<>();

        if (StringUtils.isNotEmpty(this.firstNames)) {
            params.put("firstNames", this.firstNames);
        }

        if (StringUtils.isNotEmpty(this.lastName)) {
            params.put("lastName", this.lastName);
        }

        if (StringUtils.isNotEmpty(this.dateOfBirth)) {
            params.put("dateOfBirth", this.dateOfBirth);
        }

        if (StringUtils.isNotEmpty(this.postcode)) {
            params.put("postcode", this.postcode);
        }

        if (nonNull(this.gender)) {
            params.put("gender", this.gender);
        }

        if(nonNull(this.exactFirstNamesMatch)){
            params.put("exactFirstNamesMatch", exactFirstNamesMatch);
        }
        return params;
    }
}