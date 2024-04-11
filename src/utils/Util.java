package utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.QuoteMode;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Util {

    public static final Path pathToCSVFileWithClaimsAndPremiseIds = Paths.get("data", "claimsAndPremiseIds.csv").toAbsolutePath();
    public static final Path pathToClaimsAndPremiseIdsIndex = Paths.get("data", "claimsAndPremiseIdsIndex").toAbsolutePath();
    public static final Path pathToPremisesAndPremiseIdsIndex = Paths.get("data", "premisesAndPremiseIdsIndex").toAbsolutePath();


    public static final CSVFormat defaultCSVFormat = CSVFormat.DEFAULT
            .withDelimiter(';')
            .withQuote('"')
            .withQuoteMode(QuoteMode.MINIMAL)
            .withEscape('\\');

    public static CSVParser readCSVFile(Path pathToCSVFile) {
        try {
            return defaultCSVFormat
                    .withFirstRecordAsHeader()
                    .parse(
                            new FileReader(
                                    String.valueOf(
                                            pathToCSVFile
                                    )
                            )
                    );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
