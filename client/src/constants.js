export const USERNAME = "username";
export const PASSWORD = "password";
export const STATE_KEY = "state";
export const THREE = "three";
export const TWO = "two";
export const FOUR = "four";
export const FIVE = "five";
export const SIX = "six";
export const SEVEN = "seven";
export const BLD = "bld";
export const FMC = "fmc";
export const OH = "oh";
export const CLOCK = "clock";
export const MEGAMINX = "megaminx";
export const PYRAMINX = "pyraminx";
export const SKEWB = "skewb";
export const SQUARE = "sqone";
export const FOUR_BLD = "four_bld";
export const FIVE_BLD = "five_bld";
export const MULTI = "multi";
export const MULTI_UNPROCESSED = "MULTI_UNPROCESSED";
export const EMPTY_TIMER = "-:--:---";
export const DNF = "DNF";
export const TIMER_REFRESH_RATE = 50;
export const TIMEOUT_DELAY = 1000;
export const DEBOUNCE_DELAY = 500;
export const NEW = "new";
export const DESC = "desc";
export const ASC = "asc";
export const PRODUCT_KEY = "product";
export const ID_KEY = "id";
export const NAME_KEY = "name";
export const ORDER_KEY = "order";
export const TYPE_KEY = "type";
export const CATEGORY_KEY = "category";
export const CATEGORY_ID_KEY = "categoryId";
export const CATEGORY_ORDER_KEY = "categoryOrder";
export const QUANTITY_KEY = "quantity";
export const IS_RARE_KEY = "isRare";
export const EDIT_KEY = "EDIT";
export const REMOVE_KEY = "REMOVE";
export const STRING = "string";
export const NUMBER = "number";
export const DATE = "date";
export const ICON = "icon";
export const REAL_TIME_KEY = "Real time";
export const SYMBOL_NAME_KEY = "symbolName";
export const LAST_MOVE_DATE_KEY = "lastMoveDate";
export const RECOMMENDATION_DATE_KEY = "recommendationDate";
export const RECOMMENDATION_ACTION_KEY = "recommendationAction";
export const RECOMMENDATION_MODEL_KEY = "recommendationModel";
export const AVERAGE_COST_KEY = "averageCost";
export const POSITION_VALUE_KEY = "positionValue";
export const PNL_KEY = "pnl";
export const PERCENT_PNL_KEY = "percentPnl";
export const NET_RELATIVE_POSITION_KEY = "netRelPos";
export const RECOMMENDATION_CONFIDENCE_KEY = "recommendationConfidence";
export const RECOMMENDATION_RATIONALE_KEY = "recommendationRationale";
export const PRICE_KEY = "price"
export const OPEN_KEY = "open"
export const HIGH_KEY = "high"
export const LOW_KEY = "low"
export const PERCENT_DAY_CHANGE_KEY = "percentDayChange"
export const VOLUME_KEY = "volume"
export const META = {
    KEYS: [ID_KEY, NAME_KEY, CATEGORY_KEY, QUANTITY_KEY, EDIT_KEY, REMOVE_KEY],
    DATATYPE: {
        [ID_KEY]: NUMBER,
        [NAME_KEY]: STRING,
        [CATEGORY_KEY]: STRING,
        [QUANTITY_KEY]: NUMBER,
        [EDIT_KEY]: ICON,
        [REMOVE_KEY]: ICON
    },
    VISIBLE: {
        [ID_KEY]: false,
        [NAME_KEY]: true,
        [CATEGORY_KEY]: true,
        [QUANTITY_KEY]: true,
        [EDIT_KEY]: true,
        [REMOVE_KEY]: true
    },
    DISPLAY_NAME: {
        [ID_KEY]: "",
        [NAME_KEY]: "Name",
        [CATEGORY_KEY]: "Category",
        [QUANTITY_KEY]: "Quantity",
        [EDIT_KEY]: "",
        [REMOVE_KEY]: ""
    },
    SORTABLE: {
        [ID_KEY]: true,
        [NAME_KEY]: true,
        [CATEGORY_KEY]: true,
        [QUANTITY_KEY]: true,
        [EDIT_KEY]: false,
        [REMOVE_KEY]: false
    },
    FILTERABLE: {
        [ID_KEY]: true,
        [NAME_KEY]: true,
        [CATEGORY_KEY]: true,
        [QUANTITY_KEY]: true,
        [EDIT_KEY]: false,
        [REMOVE_KEY]: false
    }
}
export const PORTFOLIO_META = {
    KEYS: [ID_KEY,
        REAL_TIME_KEY,
        SYMBOL_NAME_KEY,
        LAST_MOVE_DATE_KEY,
        RECOMMENDATION_DATE_KEY,
        RECOMMENDATION_ACTION_KEY,
        RECOMMENDATION_MODEL_KEY,
        PRICE_KEY,
        OPEN_KEY,
        HIGH_KEY,
        LOW_KEY,
        VOLUME_KEY,
        QUANTITY_KEY,
        AVERAGE_COST_KEY,
        POSITION_VALUE_KEY,
        PNL_KEY,
        PERCENT_DAY_CHANGE_KEY,
        PERCENT_PNL_KEY,
        NET_RELATIVE_POSITION_KEY,
        RECOMMENDATION_CONFIDENCE_KEY,
        RECOMMENDATION_RATIONALE_KEY,
        EDIT_KEY
    ],
    DATATYPE: {
        [ID_KEY]: NUMBER,
        [REAL_TIME_KEY]: STRING,
        [SYMBOL_NAME_KEY]: STRING,
        [LAST_MOVE_DATE_KEY]: DATE,
        [RECOMMENDATION_DATE_KEY]: DATE,
        [RECOMMENDATION_ACTION_KEY]: STRING,
        [RECOMMENDATION_MODEL_KEY]: STRING,
        [PRICE_KEY]: NUMBER,
        [OPEN_KEY]: NUMBER,
        [HIGH_KEY]: NUMBER,
        [LOW_KEY]: NUMBER,
        [PERCENT_DAY_CHANGE_KEY]: NUMBER,
        [VOLUME_KEY]: NUMBER,
        [QUANTITY_KEY]: NUMBER,
        [AVERAGE_COST_KEY]: NUMBER,
        [POSITION_VALUE_KEY]: NUMBER,
        [PNL_KEY]: NUMBER,
        [PERCENT_PNL_KEY]: NUMBER,
        [NET_RELATIVE_POSITION_KEY]: NUMBER,
        [RECOMMENDATION_CONFIDENCE_KEY]: NUMBER,
        [RECOMMENDATION_RATIONALE_KEY]: STRING,
        [EDIT_KEY]: ICON
    },
    VISIBLE: {
        [ID_KEY]: false,
        [REAL_TIME_KEY]: true,
        [SYMBOL_NAME_KEY]: true,
        [LAST_MOVE_DATE_KEY]: true,
        [RECOMMENDATION_DATE_KEY]: true,
        [RECOMMENDATION_ACTION_KEY]: true,
        [RECOMMENDATION_MODEL_KEY]: true,
        [PRICE_KEY]: true,
        [OPEN_KEY]: true,
        [HIGH_KEY]: true,
        [LOW_KEY]: true,
        [PERCENT_DAY_CHANGE_KEY]: true,
        [VOLUME_KEY]: true,
        [QUANTITY_KEY]: true,
        [AVERAGE_COST_KEY]: true,
        [POSITION_VALUE_KEY]: false,
        [PNL_KEY]: true,
        [PERCENT_PNL_KEY]: true,
        [NET_RELATIVE_POSITION_KEY]: true,
        [RECOMMENDATION_CONFIDENCE_KEY]: true,
        [RECOMMENDATION_RATIONALE_KEY]: false,
        [EDIT_KEY]: true
    },
    DISPLAY_NAME: {
        [ID_KEY]: "",
        [REAL_TIME_KEY]: "Real Time",
        [SYMBOL_NAME_KEY]: "Symbol",
        [LAST_MOVE_DATE_KEY]: "Last Move Date",
        [RECOMMENDATION_DATE_KEY]: "Rec. Date",
        [RECOMMENDATION_ACTION_KEY]: "Rec. Action",
        [RECOMMENDATION_MODEL_KEY]: "Rec. Model",
        [PRICE_KEY]: "Price",
        [OPEN_KEY]: "Open",
        [HIGH_KEY]: "High",
        [LOW_KEY]: "Low",
        [PERCENT_DAY_CHANGE_KEY]: "%Day Change",
        [VOLUME_KEY]: "Volume",
        [QUANTITY_KEY]: "Quantity",
        [AVERAGE_COST_KEY]: "Average Cost",
        [POSITION_VALUE_KEY]: "Position Value",
        [PNL_KEY]: "PNL",
        [PERCENT_PNL_KEY]: "%PNL",
        [NET_RELATIVE_POSITION_KEY]: "gross  %PNL",
        [RECOMMENDATION_CONFIDENCE_KEY]: "Rec. Confidence",
        [RECOMMENDATION_RATIONALE_KEY]: "",
        [EDIT_KEY]: ""
    },
    SORTABLE: {
        [ID_KEY]: true,
        [REAL_TIME_KEY]: false,
        [SYMBOL_NAME_KEY]: true,
        [LAST_MOVE_DATE_KEY]: true,
        [RECOMMENDATION_DATE_KEY]: true,
        [RECOMMENDATION_ACTION_KEY]: true,
        [RECOMMENDATION_MODEL_KEY]: true,
        [PRICE_KEY]: true,
        [OPEN_KEY]: true,
        [HIGH_KEY]: true,
        [LOW_KEY]: true,
        [PERCENT_DAY_CHANGE_KEY]: true,
        [VOLUME_KEY]: true,
        [QUANTITY_KEY]: true,
        [AVERAGE_COST_KEY]: true,
        [POSITION_VALUE_KEY]: true,
        [PNL_KEY]: true,
        [PERCENT_PNL_KEY]: true,
        [NET_RELATIVE_POSITION_KEY]: true,
        [RECOMMENDATION_CONFIDENCE_KEY]: true,
        [RECOMMENDATION_RATIONALE_KEY]: false,
        [EDIT_KEY]: false
    },
    FILTERABLE: {
        [ID_KEY]: true,
        [REAL_TIME_KEY]: false,
        [SYMBOL_NAME_KEY]: true,
        [LAST_MOVE_DATE_KEY]: true,
        [RECOMMENDATION_DATE_KEY]: true,
        [RECOMMENDATION_ACTION_KEY]: true,
        [RECOMMENDATION_MODEL_KEY]: true,
        [PRICE_KEY]: false,
        [OPEN_KEY]: false,
        [HIGH_KEY]: false,
        [LOW_KEY]: false,
        [PERCENT_DAY_CHANGE_KEY]: false,
        [VOLUME_KEY]: false,
        [QUANTITY_KEY]: false,
        [AVERAGE_COST_KEY]: false,
        [POSITION_VALUE_KEY]: true,
        [PNL_KEY]: false,
        [PERCENT_PNL_KEY]: false,
        [NET_RELATIVE_POSITION_KEY]: false,
        [RECOMMENDATION_CONFIDENCE_KEY]: false,
        [RECOMMENDATION_RATIONALE_KEY]: false,
        [EDIT_KEY]: false
    }
}

export const generateRandomBetweenZeroAndX = (x) => {
    return Math.floor(Math.random() * x);
};
