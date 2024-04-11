package queryProcessing;

public class ArgsMeClaim {

    String claimId;
    String claimText;
    String premiseIds;
    String claimClusterId;


    public ArgsMeClaim(String claimId, String claimText, String claimClusterId, String premiseIds) {
        this.claimId = claimId;
        this.claimText = claimText;
        this.claimClusterId = claimClusterId;
        this.premiseIds = premiseIds;
    }

    // getter
    public String getClaimId() {
        return claimId;
    }

    public String getClaimClusterId() {
        return this.claimClusterId;
    }

}
