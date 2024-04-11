package queryProcessing;

public class ArgsMePremise {

    String premiseId;
    String premiseText;
    String premiseStance;
    String claimText;
    String discussionTitle;
    String premiseClusterId;
    String dcf_cogency;
    String dcf_reasonableness;
    String dcf_effectiveness;

    public ArgsMePremise(String premiseId, String premiseText, String premiseStance, String claimText, String discussionTitle, String premiseClusterId, String dcf_cogency, String dcf_reasonableness, String dcf_effectiveness) {
        this.premiseId = premiseId;
        this.premiseText = premiseText;
        this.premiseStance = premiseStance;
        this.claimText = claimText;
        this.discussionTitle = discussionTitle;
        this.premiseClusterId = premiseClusterId;
        this.dcf_cogency = dcf_cogency;
        this.dcf_reasonableness = dcf_reasonableness;
        this.dcf_effectiveness = dcf_effectiveness;
    }

    public String getPremiseId() {
        return premiseId;
    }

    public String getPremiseText() {
        return premiseText;
    }

    public String getPremiseStance() {
        return premiseStance;
    }

    public String getClaimText() {
        return claimText;
    }

    public String getPremiseClusterId() {
        return this.premiseClusterId;
    }

    public String getDcf_cogency() {
        return dcf_cogency;
    }

    public String getDcf_reasonableness() {
        return dcf_reasonableness;
    }

    public String getDcf_effectiveness() {
        return dcf_effectiveness;
    }
}
