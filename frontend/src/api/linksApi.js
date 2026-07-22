import axiosClient from './axiosClient';

export const getLinks = () => axiosClient.get('/links').then(r => r.data);
export const createLink = (data) => axiosClient.post('/links', data).then(r => r.data);
export const updateLink = (id, data) => axiosClient.put(`/links/${id}`, data).then(r => r.data);
export const deleteLink = (id) => axiosClient.delete(`/links/${id}`);
export const reorderLinks = (orderedIds) => axiosClient.patch('/links/reorder', orderedIds);

// FormData, so the browser sets the multipart boundary itself — setting Content-Type here would
// omit it and the server could not parse the body.
export const uploadThumbnail = (id, file) => {
  const form = new FormData();
  form.append('file', file);
  return axiosClient.post(`/links/${id}/thumbnail`, form).then(r => r.data);
};

export const deleteThumbnail = (id) => axiosClient.delete(`/links/${id}/thumbnail`).then(r => r.data);
