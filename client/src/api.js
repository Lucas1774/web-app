import axios from "axios";

const MAX_DATA_LENGTH = 10000;
// Android's localhost is public cause I'm too lazy to be configuring cordova's web server in the build script
// Change private address for mobile testing
const isLocal =
  window.location.hostname === process.env.REACT_APP_PRIVATE_ADDRESS
  || (window.location.hostname === "localhost" && !/Android/i.test(navigator.userAgent));
const BASE_URL = isLocal ? `http://${process.env.REACT_APP_PRIVATE_ADDRESS}:8080` : `https://${process.env.REACT_APP_PUBLIC_ADDRESS}`;
const auth = { username: process.env.REACT_APP_SERVER_USERNAME, password: process.env.REACT_APP_SERVER_PASSWORD };
const config = {
  headers: {
    "Authorization": "Basic " + btoa(`${auth.username}:${auth.password}`),
    "Content-Type": "application/json"
  },
  withCredentials: true
};

export const post = (url, data) => {
  if (JSON.stringify(data).length > MAX_DATA_LENGTH) {
    return Promise.resolve({ data: `Data exceeds maximum length of ${MAX_DATA_LENGTH} characters.` });
  } else {
    return axios.post(BASE_URL + url, data, config);
  }
};

export const get = (url) => {
  return axios.get(BASE_URL + url, config);
};
