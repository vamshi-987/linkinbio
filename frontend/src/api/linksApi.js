import axiosClient from './axiosClient';

export const getLinks = () => axiosClient.get('/links').then(r => r.data);
export const createLink = (data) => axiosClient.post('/links', data).then(r => r.data);
export const updateLink = (id, data) => axiosClient.put(`/links/${id}`, data).then(r => r.data);
export const deleteLink = (id) => axiosClient.delete(`/links/${id}`);
export const reorderLinks = (orderedIds) => axiosClient.patch('/links/reorder', orderedIds);