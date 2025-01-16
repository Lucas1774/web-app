export const USERNAME = "username";
export const PASSWORD = "password";
export const THREE = "three";
export const TWO = "two";
export const FOUR = "four";
export const FIVE = "five";
export const SEVEN = "seven";
export const SIX = "six";
export const BLD = "bld";
export const FMC = "fmc";
export const OH = "oh";
export const CLOCK = "clock";
export const MEGAMINX = "megaminx";
export const PYRAMINX = "pyraminx";
export const SKEWB = "skewb";
export const SQUARE = "square1";
export const FOUR_BLD = "four_bld";
export const FIVE_BLD = "five_bld";
export const MULTI = "multi";
export const MULTI_UNPROCESSED = "MULTI_UNPROCESSED";
export const EMPTY_TIMER = "-:--:---";
export const DNF = "DNF";
export const TIMER_REFRESH_RATE = 50;
export const TIMEOUT_DELAY = 1000;
export const NEW = "new";
export const DESC = "desc";
export const ASC = "asc";
export const ID_KEY = "ID";
export const NAME_KEY = "NAME";
export const CATEGORY_KEY = "CATEGORY";
export const CATEGORY_ID_KEY = "CATEGORY_ID";
export const CATEGORY_ORDER_KEY = "ORDER";
export const QUANTITY_KEY = "QUANTITY";
export const EDIT_KEY = "EDIT";
export const REMOVE_KEY = "REMOVE";
export const STRING = "string";
export const NUMBER = "number";
export const ICON = "icon";
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

export const generateRandomBetweenZeroAndX = (x) => {
    return Math.floor(Math.random() * x);
};
