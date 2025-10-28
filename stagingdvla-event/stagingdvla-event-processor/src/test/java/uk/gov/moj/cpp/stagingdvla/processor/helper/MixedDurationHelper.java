package uk.gov.moj.cpp.stagingdvla.processor.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class MixedDurationHelper {

    private static final char[] VALID_REGULAR_TYPES = {'A', 'P', 'C', 'E'};
    private static final char[] VALID_SPECIAL_TYPES = {'J', 'M'};
    private static final char[] INVALID_TYPES = {'B', 'F', 'K', 'L'};
    private static final String[] PERIODS = {"Y", "M", "W", "D", "H"};
    private static final int[] VALID_VALUES = {1, 9, 10, 99};
    private static final int[] INVALID_VALUES = {100, 999};

    private static class GeneratorState {
        Set<String> generatedCodes;
        List<Integer> currentCombination;
        boolean isSingleDuration;
        int startIndex;

        GeneratorState() {
            this.generatedCodes = new HashSet<>();
            this.currentCombination = new ArrayList<>(Collections.nCopies(PERIODS.length, 0));
            this.isSingleDuration = true;
            this.startIndex = 0;
        }

        void reset() {
            generatedCodes.clear();
            java.util.Collections.fill(currentCombination, 0);
            isSingleDuration = true;
            startIndex = 0;
        }
    }

    private static final Map<Character, GeneratorState> stateMap = new HashMap<>();

    static {
        for (char type : VALID_REGULAR_TYPES) {
            stateMap.put(type, new GeneratorState());
        }
        for (char type : VALID_SPECIAL_TYPES) {
            stateMap.put(type, new GeneratorState());
        }
    }

    public static String generateNextImprisonmentMixedPeriod(Pattern regexPattern) {
        return generateNextCode('A', regexPattern, false);
    }

    public static String generateNextYouthCustodySentenceMixedPeriod(Pattern regexPattern) {
        return generateNextCode('P', regexPattern, false);
    }

    public static String generateNextSuspendedPrisonSentenceMixedPeriod(Pattern regexPattern) {
        return generateNextCode('C', regexPattern, false);
    }

    public static String generateNextConditionalDischargeMixedPeriod(Pattern regexPattern) {
        return generateNextCode('E', regexPattern, false);
    }

    public static String generateNextAbsoluteDischargeMixedPeriod(Pattern regexPattern) {
        return generateNextCode('J', regexPattern, true);
    }

    public static String generateNextCommunityOrderSentenceMixedPeriod(Pattern regexPattern) {
        return generateNextCode('M', regexPattern, true);
    }

    private static String generateNextCode(char type, Pattern regexPattern, boolean isSpecial) {
        GeneratorState state = stateMap.get(type);
        while (true) {
            String code = generateCode(type, isSpecial, state);
            if (code == null) {
                state.reset();
                return null;
            }
            if (regexPattern.matcher(code).matches() && !state.generatedCodes.contains(code)) {
                state.generatedCodes.add(code);
                return code;
            }
        }
    }

    private static String generateCode(char type, boolean isSpecial, GeneratorState state) {
        StringBuilder code = new StringBuilder();
        code.append(type);
        if (isSpecial) {
            code.append("000");
        }

        if (state.isSingleDuration) {
            for (int i = state.startIndex; i < PERIODS.length; i++) {
                if (state.currentCombination.get(i) < VALID_VALUES.length) {
                    code.append(String.format("%02d", VALID_VALUES[state.currentCombination.get(i)])).append(PERIODS[i]);
                    break;
                }
            }
        } else {
            boolean hasValue = false;
            for (int i = state.startIndex; i < PERIODS.length; i++) {
                if (state.currentCombination.get(i) < VALID_VALUES.length) {
                    code.append(VALID_VALUES[state.currentCombination.get(i)]).append(PERIODS[i]);
                    hasValue = true;
                }
            }
            if (!hasValue) {
                code.append(VALID_VALUES[0]).append(PERIODS[state.startIndex]);
            }
        }

        if (incrementState(state)) {
            return null;
        }

        return code.toString();
    }

    private static boolean incrementState(GeneratorState state) {
        if (state.isSingleDuration) {
            for (int i = state.startIndex; i < PERIODS.length; i++) {
                int newValue = state.currentCombination.get(i) + 1;
                if (newValue < VALID_VALUES.length) {
                    state.currentCombination.set(i, newValue);
                    return false;
                }
                state.currentCombination.set(i, 0);
            }
            state.isSingleDuration = false;
            return false;
        }

        for (int i = PERIODS.length - 1; i >= state.startIndex; i--) {
            int newValue = state.currentCombination.get(i) + 1;
            if (newValue < VALID_VALUES.length) {
                state.currentCombination.set(i, newValue);
                return false;
            }
            state.currentCombination.set(i, 0);
        }

        state.startIndex++;
        if (state.startIndex >= PERIODS.length) {
            return true;
        }
        state.isSingleDuration = true;
        return false;
    }

    public static String generateNextInvalidCode(Pattern singleCharacterOtherSentenceTypeRegexPattern, Pattern multipleCharacterOtherSentenceTypeRegexPattern) {
        for (char type : INVALID_TYPES) {
            StringBuilder code = new StringBuilder();
            code.append(type);

            boolean hasValue = false;
            for (String period : PERIODS) {
                if (Math.random() < 0.5) {
                    code.append(INVALID_VALUES[(int) (Math.random() * INVALID_VALUES.length)]).append(period);
                    hasValue = true;
                }
            }

            if (!hasValue) {
                code.append(INVALID_VALUES[0]).append(PERIODS[0]);
            }

            if (!isValidCode(code.toString(), singleCharacterOtherSentenceTypeRegexPattern, multipleCharacterOtherSentenceTypeRegexPattern)) {
                return code.toString();
            }
        }
        return null;
    }

    private static boolean isValidCode(String code, Pattern singleCharacterOtherSentenceTypeRegexPattern, Pattern multipleCharacterOtherSentenceTypeRegexPattern) {
        return singleCharacterOtherSentenceTypeRegexPattern.matcher(code).matches() || multipleCharacterOtherSentenceTypeRegexPattern.matcher(code).matches();
    }
}