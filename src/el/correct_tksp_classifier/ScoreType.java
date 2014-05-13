package el.correct_tksp_classifier;

public enum ScoreType {
    // Eval of each test case to see if the positive token span ranks the highest or not using the decision function w^T * x
    LIBLINEAR_SCORE,
    // Eval of each test case to see if the positive token span ranks the highest or not using the decision function p(n|e)
    CROSSWIKIS_SCORE_ONLY,
    // Eval of each test case to see if the positive token span ranks the highest or not using the decision function #tokens
    LONGEST_TKSP_SCORE
}
