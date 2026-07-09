import axiosClient from './axiosClient';

export const updateProfile = (data) => axiosClient.patch('/profile', data);