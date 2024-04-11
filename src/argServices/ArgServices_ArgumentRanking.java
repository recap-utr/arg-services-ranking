package argServices;

import de.uni_trier.recap.arg_services.ranking.v1beta.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import io.grpc.stub.StreamObserver;
import queryProcessing.Probability;
import queryProcessing.QueryProcessor;
import queryProcessing.Result;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;



public class ArgServices_ArgumentRanking {

    private static final Logger logger = Logger.getLogger(ArgServices_ArgumentRanking.class.getName());
    private static final int rpcPort = 50200;

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        server =
                ServerBuilder.forPort(rpcPort)
                        .addService(new RankingServiceImpl())
                        .addService(ProtoReflectionService.newInstance())
                        .build()
                        .start();
        logger.info("Server started, listening on " + rpcPort);
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    // Use stderr here since the logger may have been reset by its JVM shutdown
                                    // hook.
                                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                                    try {
                                        ArgServices_ArgumentRanking.this.stop();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace(System.err);
                                    }
                                    System.err.println("*** server shut down");
                                }));
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    /** Await termination on the main thread since the grpc library uses daemon threads. */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /** Main launches the server from the command line. */
    public static void main(String[] args) throws IOException, InterruptedException {
        final var server = new ArgServices_ArgumentRanking();
        server.start();
        server.blockUntilShutdown();
    }

    static class RankingServiceImpl extends RankingServiceGrpc.RankingServiceImplBase {

        QueryProcessor queryProcessor_quality;
        QueryProcessor queryProcessor_statistical;

        public RankingServiceImpl() {
            super();
            queryProcessor_quality = new QueryProcessor();
            queryProcessor_statistical = new QueryProcessor();
        }

        @Override
        public void statisticalRanking(StatisticalRankingRequest request, StreamObserver<StatisticalRankingResponse> responseObserver)
                throws IllegalArgumentException {

            String query = request.getQuery();

            Map<String, List<Result>> map_premiseClusterId_result = queryProcessor_statistical.processQuery(
                    query,
                    QueryProcessor.defaultSimilarityMethodForClaimRetrieval,
                    10,
                    Probability.ProbabilityType.pfIcf
            );

            var response = StatisticalRankingResponse.newBuilder();

            for (Map.Entry<String, List<Result>> entry_premiseClusterId_result : map_premiseClusterId_result.entrySet()) {
                List<Result> results = entry_premiseClusterId_result.getValue();
                for (Result result : results) {
                    var aduBuilder = de.uni_trier.recap.arg_services.ranking.v1beta.StatisticalRankedAdu.newBuilder();

                    aduBuilder.setText(result.getPremiseText());
                    aduBuilder.setScore(result.getClusterProbability());

                    response.addRankedAdus(
                            aduBuilder.build()
                    );
                }
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

        }

        @Override
        public void qualityRanking(QualityRankingRequest request, StreamObserver<QualityRankingResponse> responseObserver)
                throws IllegalArgumentException {

            String query = request.getQuery();

            Map<String, List<Result>> map_premiseClusterId_result = queryProcessor_quality.processQuery(
                    query,
                    QueryProcessor.defaultSimilarityMethodForClaimRetrieval,
                    10,
                    Probability.ProbabilityType.dcfsAggregated
            );

            var response = QualityRankingResponse.newBuilder();

            for (Map.Entry<String, List<Result>> entry_premiseClusterId_result : map_premiseClusterId_result.entrySet()) {
                List<Result> results = entry_premiseClusterId_result.getValue();
                for (Result result : results) {
                    var aduBuilder = de.uni_trier.recap.arg_services.ranking.v1beta.QualityRankedAdu.newBuilder();

                    aduBuilder.setText(result.getPremiseText());
                    aduBuilder.setGlobalQuality(result.getClusterProbability());

                    response.addRankedAdus(
                            aduBuilder.build()
                    );
                }
            }

            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }

}
