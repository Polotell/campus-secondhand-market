import request from '@/utils/request'

/** @returns {Promise<Array<{id:number,imageUrl:string,linkUrl?:string,sort:number,status:string}>>} */
export const listCarousels = () =>
  request.get('/home/carousels', { silent: true })
