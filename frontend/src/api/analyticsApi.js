import axiosClient from './axiosClient';

export const getAnalyticsSummary = () => axiosClient.get('/analytics/summary').then(r => r.data);