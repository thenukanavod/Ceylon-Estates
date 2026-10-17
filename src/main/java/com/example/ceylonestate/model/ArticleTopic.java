package com.example.ceylonestate.model;

/** What an article is about; used for the filter chips and the card colour. */
public enum ArticleTopic {
    BUYING("Buying & legal", "bi-file-earmark-text", "linear-gradient(135deg, #1565c0, #42a5f5)"),
    FINANCE("Loans & finance", "bi-bank", "linear-gradient(135deg, #2e7d32, #66bb6a)"),
    MARKET("Market news", "bi-graph-up-arrow", "linear-gradient(135deg, #e63946, #ff8fa3)");

    private final String label;
    private final String icon;
    private final String gradient;

    ArticleTopic(String label, String icon, String gradient) {
        this.label = label;
        this.icon = icon;
        this.gradient = gradient;
    }

    public String getLabel() { return label; }
    public String getIcon() { return icon; }
    public String getGradient() { return gradient; }
}
