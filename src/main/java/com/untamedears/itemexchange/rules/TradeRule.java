package com.untamedears.itemexchange.rules;

public final class TradeRule {

    private ExchangeRule input;
    private ExchangeRule output;

    public TradeRule() {
    }

    public boolean isValid() {
        return this.input != null;
    }

    public ExchangeRule getInput() {
        return this.input;
    }

    public void setInput(ExchangeRule input) {
        this.input = input;
    }

    public boolean hasOutput() {
        return this.output != null;
    }

    public ExchangeRule getOutput() {
        return this.output;
    }

    public void setOutput(ExchangeRule output) {
        this.output = output;
    }

}
