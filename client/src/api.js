import axios from "axios";

const MAX_DATA_LENGTH = 10000;
// Android's localhost is public cause I'm too lazy to be configuring cordova's web server in the build script
// Change private address for mobile testing
const isLocal =
  globalThis.location.hostname === process.env.REACT_APP_PRIVATE_ADDRESS
  || (globalThis.location.hostname === "localhost" && !/Android/i.test(navigator.userAgent));
const BASE_URL = isLocal ? `http://${process.env.REACT_APP_PRIVATE_ADDRESS}:8080` : `https://${process.env.REACT_APP_PUBLIC_ADDRESS}`;
const auth = { username: process.env.REACT_APP_SERVER_USERNAME, password: process.env.REACT_APP_SERVER_PASSWORD };
let csrfToken;
const getConfig = () => ({
  headers: {
    "Authorization": "Basic " + btoa(`${auth.username}:${auth.password}`),
    "Content-Type": "application/json",
    ...(csrfToken && { "X-XSRF-TOKEN": csrfToken })
  },
  withCredentials: true
});

const initializeCsrf = async () => {
  if (!csrfToken) {
    const response = await axios.get(BASE_URL + "/authentication/csrf", {
      headers: {
        "Authorization": "Basic " + btoa(`${auth.username}:${auth.password}`)
      },
      withCredentials: true
    });
    csrfToken = response.data.token;
  }
};

export const post = async (url, data) => {
  if (JSON.stringify(data).length > MAX_DATA_LENGTH) {
    return { data: `Data exceeds maximum length of ${MAX_DATA_LENGTH} characters.` };
  }

  await initializeCsrf();
  return axios.post(BASE_URL + url, data, getConfig());
};

export const get = (url) => {
  return axios.get(BASE_URL + url, getConfig());
};
