package group11.assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Dictionary entry for result map that contains conversion result, location of
 * converted result,
 * and error (if occurred)
 */
public class AssemblerConverterResult {
    String error = "";
    Integer conversionResult = -1;
    Integer location = -1;

    AssemblerConverterResult(Integer conversionResult, Integer location, String error) {
        this.error = error;
        this.conversionResult = conversionResult;
        this.location = location;
    }

    Integer getLocation() {
        return this.location;
    }

    String getError() {
        return this.error;
    }

    String getResultInOctal() {
        String formattedLocation = String.format("%06o", this.location);
        String formattedConversionResult = String.format("%06o", this.conversionResult);
        return formattedLocation + " " + formattedConversionResult;
    }

    Integer getConversionResult() {
        return this.conversionResult;
    }

    public String toString() {
        return "Result [Conversion: " + this.conversionResult + ", error " + this.error + "]";
    }

}
