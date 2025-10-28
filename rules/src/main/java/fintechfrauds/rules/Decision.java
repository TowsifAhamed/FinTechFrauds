package fintechfrauds.rules;

import java.util.List;

public record Decision(double risk, String action, List<String> reasons) {}
