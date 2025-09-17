package group11.assembler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;


public class AssemblerConverterResult {
    String error = "";
    Integer conversionResult = -1;

    AssemblerConverterResult(Integer conversionResult, String error) {
        this.error = error;
        this.conversionResult = conversionResult;
    }

    String getError() {
        return this.error;
    }

    String getResultInOctal(){
    return String.format("%06o", this.conversionResult);    
    }

    Integer getConversionResult() {
        return this.conversionResult;
    }

    public String toString() {
        return "Result [Conversion: " + this.conversionResult + ", error " + this.error + "]";
    }


}
