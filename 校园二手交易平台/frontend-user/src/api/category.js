import request from '@/utils/request'

/**
 * 获取分类树（两级）
 * @returns {Promise<Array<{id:number, name:string, children:Array}>>}
 */
export const getCategoryTree = () => request.get('/categories')
