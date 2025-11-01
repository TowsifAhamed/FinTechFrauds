package fintechfrauds.serve.api.dto;

import java.util.List;

public record ScoreResponse(double risk, String decision, List<String> reasons) {}
