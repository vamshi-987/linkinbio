import axiosClient from './axiosClient';

export const getAnalyticsSummary = () => axiosClient.get('/analytics/summary').then(r => r.data);

/** Country / device / referrer splits, served from the pre-aggregated rollups. */
export const getAnalyticsBreakdown = () => axiosClient.get('/analytics/breakdown').then(r => r.data);
