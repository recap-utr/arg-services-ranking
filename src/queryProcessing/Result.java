package queryProcessing;

public class Result {

    String queryClaim;
    String resultClaimId;
    String resultClaimText;
    String resultClaimClusterId;

    double resultClaimSimilarityScoreToQuery;
    double normalization_claimRetrieval;

    String premiseId;
    String premiseText;
    String premiseStance;
    String premiseClusterId;

    int pf;
    int cf;
    double icf;
    double normalization_pfIcf;

    double cogency;
    double reasonableness;
    double effectiveness;
    double normalization_cogency;
    double normalization_reasonableness;
    double normalization_effectiveness;

    double singleProbability;
    double clusterProbability;

    boolean isRepresentative;

    public Result (
            String queryClaim,
            String resultClaimId,
            String resultClaimText,
            String resultClaimClusterId,

            double resultClaimSimilarityScoreToQuery,
            double normalization_claimRetrieval,
            String premiseId,
            String premiseText,
            String premiseStance,
            String premiseClusterId,
            int pf,
            int cf,
            double icf,
            double normalization_pfIcf,

            double cogency,
            double reasonableness,
            double effectiveness,
            double normalization_cogency,
            double normalization_reasonableness,
            double normalization_effectiveness,

            Probability.ProbabilityType probabilityType,
            double clusterProbability
    ) {

        this.queryClaim = queryClaim;
        this.resultClaimId = resultClaimId;
        this.resultClaimText = resultClaimText;
        this.resultClaimClusterId = resultClaimClusterId;
        this.resultClaimSimilarityScoreToQuery = resultClaimSimilarityScoreToQuery;
        this.normalization_claimRetrieval = normalization_claimRetrieval;
        this.premiseId = premiseId;
        this.premiseText = premiseText;
        this.premiseStance = premiseStance;
        this.premiseClusterId = premiseClusterId;
        this.pf = pf;
        this.cf = cf;
        this.icf = icf;
        this.normalization_pfIcf = normalization_pfIcf;
        this.cogency = cogency;
        this.reasonableness = reasonableness;
        this.effectiveness = effectiveness;
        this.normalization_cogency = normalization_cogency;
        this.normalization_reasonableness = normalization_reasonableness;
        this.normalization_effectiveness = normalization_effectiveness;
        this.singleProbability = getSingleProbability(probabilityType);
        this.clusterProbability = clusterProbability;

        this.isRepresentative = false;
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

    public String getPremiseClusterId() {
        return premiseClusterId;
    }

    public int getPf() {
        return pf;
    }

    public int getCf() {
        return cf;
    }

    public double getIcf() {
        return icf;
    }

    public double getSingleProbability(Probability.ProbabilityType probabilityType) {
        switch (probabilityType) {
            case pfIcf:
                return getProbabilityOfResultClaimSimilarityScoreToQuery() * getProbPfIcf();
            case dcfsAggregated:
                return getProbabilityOfResultClaimSimilarityScoreToQuery() * getProbDCFsAggregated();
            case average:
                return getProbabilityOfResultClaimSimilarityScoreToQuery() * getProbAverage_pfIcf_DCFsCombined();
            default:
                return 0;
        }
    }

    public double getClusterProbability() {
        return clusterProbability;
    }

    public boolean isRepresentative() {
        return isRepresentative;
    }

    public double getEffectiveness() {
        return effectiveness;
    }

    public double getProbabilityOfResultClaimSimilarityScoreToQuery () {
        return this.resultClaimSimilarityScoreToQuery / normalization_claimRetrieval;
    }

    public double getPfIcf () {
        return this.pf * this.icf;
    }
    public double getNormalization_pfIcf () {
        return this.normalization_pfIcf;
    }
    public double getProbPfIcf () {
        return getPfIcf() / getNormalization_pfIcf();
    }

    public double getNormalization_cogency () {
        return this.normalization_cogency;
    }

    public double getNormalization_reasonableness () {
        return this.normalization_reasonableness;
    }

    public double getNormalization_effectiveness () {
        return this.normalization_effectiveness;
    }

    public double getDCFsAggregated () {
        return this.cogency + this.reasonableness + this.effectiveness;
    }
    public double getNormalization_DCFsAggregated () {
        return getNormalization_cogency() + getNormalization_reasonableness() + getNormalization_effectiveness();
    }
    public double getProbDCFsAggregated () {
        return getDCFsAggregated() / getNormalization_DCFsAggregated();
    }

    public double getProbAverage_pfIcf_DCFsCombined () {
        return (getProbPfIcf() + getProbDCFsAggregated()) / 2;
    }


    public void setClusterProbability (double clusterProbability) {
        this.clusterProbability = clusterProbability;
    }
}
