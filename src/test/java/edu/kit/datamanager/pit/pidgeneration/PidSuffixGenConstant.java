package edu.kit.datamanager.pit.pidgeneration;

public class PidSuffixGenConstant implements PidSuffixGenerator {

    private String suffix = "tEsT123";

    public PidSuffixGenConstant() {}

    public PidSuffixGenConstant(String suffix) {
        this.suffix = suffix;
    }

    @Override
    public PidSuffix generate() {
        return new PidSuffix(this.suffix);
    }
    
}
