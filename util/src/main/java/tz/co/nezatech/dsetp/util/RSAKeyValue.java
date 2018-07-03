package tz.co.nezatech.dsetp.util;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.math.BigInteger;

@JacksonXmlRootElement
public class RSAKeyValue {
    @JacksonXmlProperty(localName = "Modulus")
    private String modulus;
    @JacksonXmlProperty(localName = "Exponent")
    private String exponent;

    public String getModulus() {
        return modulus;
    }

    public void setModulus(String modulus) {
        this.modulus = modulus;
    }

    public String getExponent() {
        return exponent;
    }

    public void setExponent(String exponent) {
        this.exponent = exponent;
    }
}
