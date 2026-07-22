import axiosClient from './axiosClient';

export const getProfile = () => axiosClient.get('/profile').then((res) => res.data);

export const updateProfile = (data) => axiosClient.patch('/profile', data);

export const uploadAvatar = (file) => {
  const form = new FormData();
  form.append('file', file);
  return axiosClient.post('/profile/avatar', form).then((res) => res.data);
};

export const deleteAvatar = () => axiosClient.delete('/profile/avatar').then((res) => res.data);
