import axios from 'axios';
import { getAccessToken, setAccessToken } from './tokenStore';

const baseURL = import.meta.env.VITE_API_BASE_URL;

const client = axios.create({
  baseURL,
  withCredentials: true,
});

client.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let reissuePromise = null;

function reissueAccessToken() {
  if (!reissuePromise) {
    reissuePromise = client
      .post('/api/auth/reissue')
      .then((res) => {
        const newToken = res.data.data.accessToken;
        setAccessToken(newToken);
        return newToken;
      })
      .finally(() => {
        reissuePromise = null;
      });
  }
  return reissuePromise;
}

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error;
    const isReissueCall = config?.url?.includes('/api/auth/reissue');

    if (response?.status === 401 && !isReissueCall && !config._retried) {
      config._retried = true;
      try {
        const newToken = await reissueAccessToken();
        config.headers.Authorization = `Bearer ${newToken}`;
        return client(config);
      } catch {
        setAccessToken(null);
      }
    }

    return Promise.reject(error);
  },
);

export default client;
