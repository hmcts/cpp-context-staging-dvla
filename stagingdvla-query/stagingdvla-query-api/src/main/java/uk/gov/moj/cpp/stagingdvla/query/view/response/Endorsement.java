package uk.gov.moj.cpp.stagingdvla.query.view.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public class Endorsement implements Serializable {

    private static final long serialVersionUID = 1L;

    private String appealCourtCode;
    private LocalDate appealDate;
    private LocalDate convictionDate;
    private String convictionCourtCode;
    private Disqualification disqualification;
    private LocalDate disqualificationSuspendedPendingAppealDate;
    private LocalDate disqualificationReimposedDate;
    private LocalDate disqualificationRemovalDate;
    private String disqualifiedPendingSentence;
    private LocalDate expiryDate;
    private BigDecimal fine;
    private LocalDate fromDate;
    private String identifier;
    private Intoxicant intoxicant;
    private Markers markers;
    private LocalDate nextReportDate;
    private String notificationSource;
    private String offenceCode;
    private String offenceLegalLiteral;
    private LocalDate offenceDate;
    private String otherSentence;
    private Integer penaltyPoints;
    private PrisonSentenceSuspendedPeriod prisonSentenceSuspendedPeriod;
    private Boolean rehabilitationCourseCompleted;
    private LocalDate sentenceDate;
    private String sentencingCourtCode;

    public static class Builder {
        private String appealCourtCode;
        private LocalDate appealDate;
        private LocalDate convictionDate;
        private String convictionCourtCode;
        private Disqualification disqualification;
        private LocalDate disqualificationSuspendedPendingAppealDate;
        private LocalDate disqualificationReimposedDate;
        private LocalDate disqualificationRemovalDate;
        private String disqualifiedPendingSentence;
        private LocalDate expiryDate;
        private BigDecimal fine;
        private LocalDate fromDate;
        private String identifier;
        private Intoxicant intoxicant;
        private Markers markers;
        private LocalDate nextReportDate;
        private String notificationSource;
        private String offenceCode;
        private String offenceLegalLiteral;
        private LocalDate offenceDate;
        private String otherSentence;
        private Integer penaltyPoints;
        private PrisonSentenceSuspendedPeriod prisonSentenceSuspendedPeriod;
        private Boolean rehabilitationCourseCompleted;
        private LocalDate sentenceDate;
        private String sentencingCourtCode;

        public Builder appealCourtCode(String appealCourtCode) {
            this.appealCourtCode = appealCourtCode;
            return this;
        }

        public Builder appealDate(LocalDate appealDate) {
            this.appealDate = appealDate;
            return this;
        }

        public Builder convictionDate(LocalDate convictionDate) {
            this.convictionDate = convictionDate;
            return this;
        }

        public Builder convictionCourtCode(String convictionCourtCode) {
            this.convictionCourtCode = convictionCourtCode;
            return this;
        }

        public Builder disqualification(Disqualification disqualification) {
            this.disqualification = disqualification;
            return this;
        }

        public Builder disqualificationSuspendedPendingAppealDate(LocalDate disqualificationSuspendedPendingAppealDate) {
            this.disqualificationSuspendedPendingAppealDate = disqualificationSuspendedPendingAppealDate;
            return this;
        }

        public Builder disqualificationReimposedDate(LocalDate disqualificationReimposedDate) {
            this.disqualificationReimposedDate = disqualificationReimposedDate;
            return this;
        }

        public Builder disqualificationRemovalDate(LocalDate disqualificationRemovalDate) {
            this.disqualificationRemovalDate = disqualificationRemovalDate;
            return this;
        }

        public Builder disqualifiedPendingSentence(String disqualifiedPendingSentence) {
            this.disqualifiedPendingSentence = disqualifiedPendingSentence;
            return this;
        }

        public Builder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public Builder fine(BigDecimal fine) {
            this.fine = fine;
            return this;
        }

        public Builder fromDate(LocalDate fromDate) {
            this.fromDate = fromDate;
            return this;
        }

        public Builder identifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public Builder intoxicant(Intoxicant intoxicant) {
            this.intoxicant = intoxicant;
            return this;
        }

        public Builder markers(Markers markers) {
            this.markers = markers;
            return this;
        }

        public Builder nextReportDate(LocalDate nextReportDate) {
            this.nextReportDate = nextReportDate;
            return this;
        }

        public Builder notificationSource(String notificationSource) {
            this.notificationSource = notificationSource;
            return this;
        }

        public Builder offenceCode(String offenceCode) {
            this.offenceCode = offenceCode;
            return this;
        }

        public Builder offenceLegalLiteral(String offenceLegalLiteral) {
            this.offenceLegalLiteral = offenceLegalLiteral;
            return this;
        }

        public Builder offenceDate(LocalDate offenceDate) {
            this.offenceDate = offenceDate;
            return this;
        }

        public Builder otherSentence(String otherSentence) {
            this.otherSentence = otherSentence;
            return this;
        }

        public Builder penaltyPoints(Integer penaltyPoints) {
            this.penaltyPoints = penaltyPoints;
            return this;
        }

        public Builder prisonSentenceSuspendedPeriod(PrisonSentenceSuspendedPeriod prisonSentenceSuspendedPeriod) {
            this.prisonSentenceSuspendedPeriod = prisonSentenceSuspendedPeriod;
            return this;
        }

        public Builder rehabilitationCourseCompleted(Boolean rehabilitationCourseCompleted) {
            this.rehabilitationCourseCompleted = rehabilitationCourseCompleted;
            return this;
        }

        public Builder sentenceDate(LocalDate sentenceDate) {
            this.sentenceDate = sentenceDate;
            return this;
        }

        public Builder sentencingCourtCode(String sentencingCourtCode) {
            this.sentencingCourtCode = sentencingCourtCode;
            return this;
        }

        public Endorsement build() {
            return new Endorsement(appealCourtCode, appealDate, convictionDate, convictionCourtCode, disqualification, disqualificationSuspendedPendingAppealDate, disqualificationReimposedDate, disqualificationRemovalDate, disqualifiedPendingSentence, expiryDate, fine, fromDate, identifier, intoxicant, markers, nextReportDate, notificationSource, offenceCode, offenceLegalLiteral, offenceDate, otherSentence, penaltyPoints, prisonSentenceSuspendedPeriod, rehabilitationCourseCompleted, sentenceDate, sentencingCourtCode);
        }
    }

    @SuppressWarnings("squid:S00107")
    private Endorsement(String appealCourtCode, LocalDate appealDate, LocalDate convictionDate, String convictionCourtCode, Disqualification disqualification, LocalDate disqualificationSuspendedPendingAppealDate, LocalDate disqualificationReimposedDate, LocalDate disqualificationRemovalDate, String disqualifiedPendingSentence, LocalDate expiryDate, BigDecimal fine, LocalDate fromDate, String identifier, Intoxicant intoxicant, Markers markers, LocalDate nextReportDate, String notificationSource, String offenceCode, String offenceLegalLiteral, LocalDate offenceDate, String otherSentence, Integer penaltyPoints, PrisonSentenceSuspendedPeriod prisonSentenceSuspendedPeriod, Boolean rehabilitationCourseCompleted, LocalDate sentenceDate, String sentencingCourtCode) {
        this.appealCourtCode = appealCourtCode;
        this.appealDate = appealDate;
        this.convictionDate = convictionDate;
        this.convictionCourtCode = convictionCourtCode;
        this.disqualification = disqualification;
        this.disqualificationSuspendedPendingAppealDate = disqualificationSuspendedPendingAppealDate;
        this.disqualificationReimposedDate = disqualificationReimposedDate;
        this.disqualificationRemovalDate = disqualificationRemovalDate;
        this.disqualifiedPendingSentence = disqualifiedPendingSentence;
        this.expiryDate = expiryDate;
        this.fine = fine;
        this.fromDate = fromDate;
        this.identifier = identifier;
        this.intoxicant = intoxicant;
        this.markers = markers;
        this.nextReportDate = nextReportDate;
        this.notificationSource = notificationSource;
        this.offenceCode = offenceCode;
        this.offenceLegalLiteral = offenceLegalLiteral;
        this.offenceDate = offenceDate;
        this.otherSentence = otherSentence;
        this.penaltyPoints = penaltyPoints;
        this.prisonSentenceSuspendedPeriod = prisonSentenceSuspendedPeriod;
        this.rehabilitationCourseCompleted = rehabilitationCourseCompleted;
        this.sentenceDate = sentenceDate;
        this.sentencingCourtCode = sentencingCourtCode;
    }

    public Endorsement(){}

    public String getAppealCourtCode() {
        return appealCourtCode;
    }

    public LocalDate getAppealDate() {
        return appealDate;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public String getConvictionCourtCode() {
        return convictionCourtCode;
    }

    public Disqualification getDisqualification() {
        return disqualification;
    }

    public LocalDate getDisqualificationSuspendedPendingAppealDate() {
        return disqualificationSuspendedPendingAppealDate;
    }

    public LocalDate getDisqualificationReimposedDate() {
        return disqualificationReimposedDate;
    }

    public LocalDate getDisqualificationRemovalDate() {
        return disqualificationRemovalDate;
    }

    public String getDisqualifiedPendingSentence() {
        return disqualifiedPendingSentence;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Intoxicant getIntoxicant() {
        return intoxicant;
    }

    public Markers getMarkers() {
        return markers;
    }

    public LocalDate getNextReportDate() {
        return nextReportDate;
    }

    public String getNotificationSource() {
        return notificationSource;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public String getOffenceLegalLiteral() {
        return offenceLegalLiteral;
    }

    public LocalDate getOffenceDate() {
        return offenceDate;
    }

    public String getOtherSentence() {
        return otherSentence;
    }

    public Integer getPenaltyPoints() {
        return penaltyPoints;
    }

    public PrisonSentenceSuspendedPeriod getPrisonSentenceSuspendedPeriod() {
        return prisonSentenceSuspendedPeriod;
    }

    public Boolean getRehabilitationCourseCompleted() {
        return rehabilitationCourseCompleted;
    }

    public LocalDate getSentenceDate() {
        return sentenceDate;
    }

    public String getSentencingCourtCode() {
        return sentencingCourtCode;
    }
}
