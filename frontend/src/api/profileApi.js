import axiosClient from './axiosClient';

export const getProfile = () => axiosClient.get('/profile').then((res) => res.data);

export const updateProfile = (data) => axiosClient.patch('/profile', data);