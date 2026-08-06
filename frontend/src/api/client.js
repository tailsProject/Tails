// 공통 axios 인스턴스, 토큰 첨부와 401 재발급 처리
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

// 동시에 여러 요청이 401을 맞아도 재발급 호출은 한 번만 나가도록 공유
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
