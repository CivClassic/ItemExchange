package com.untamedears.itemexchange.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.inventory.Inventory;

public final class TradeRule {

    private ExchangeRule input;
    private ExchangeRule output;

    public TradeRule() {
    }

    public TradeRule(ExchangeRule input, ExchangeRule output) {
        this.input = input;
        this.output = output;
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

    public List<String> getDisplayedInfo() {
        if (!isValid()) {
            return Collections.emptyList();
        }
        List<String> info = new ArrayList<>(this.input.getDisplayedInfo());
        if (this.output != null) {
            info.addAll(this.output.getDisplayedInfo());
        }
        return info;
    }

    public boolean attemptTransaction(Inventory shop, Inventory buyer) {
        return false;
    }

}
