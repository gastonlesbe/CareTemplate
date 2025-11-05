package com.gastonlesbegueris.caretemplate.ui;

public class SubjectCostSummary {
    public String subjectId;
    public String subjectName;
    public double planned;   // suma eventos NO realizados (cost != null, realized==0)
    public double realized;  // suma eventos realizados (realized==1)
    public int count;        // cantidad de eventos en el mes

    public SubjectCostSummary(String subjectId, String subjectName) {
        this.subjectId = subjectId;
        this.subjectName = subjectName;
    }
}
