import type {GraphData,Option,Recipe,RecipeDetail,SearchRequest} from '../types'

const base=(import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080').replace(/\/$/,'')
export class ApiError extends Error { status:number; constructor(message:string,status:number){super(message);this.status=status} }
async function request<T>(path:string,init?:RequestInit):Promise<T>{
  try{
    const response=await fetch(`${base}${path}`,{...init,headers:{'Content-Type':'application/json',...init?.headers}})
    if(!response.ok){const body=await response.json().catch(()=>null) as {message?:string}|null;throw new ApiError(body?.message||'FlavorGraph is temporarily unavailable.',response.status)}
    return response.json() as Promise<T>
  }catch(error){if(error instanceof ApiError)throw error;throw new ApiError('FlavorGraph is temporarily unavailable. Please try again shortly.',0)}
}
export const api={
  options:()=>Promise.all(['ingredients','cuisines','diets','appliances'].map(x=>request<Option[]>(`/api/${x}`))).then(([ingredients,cuisines,diets,appliances])=>({ingredients,cuisines,diets,appliances})),
  search:(body:SearchRequest)=>request<Recipe[]>('/api/recipes/search',{method:'POST',body:JSON.stringify(body)}),
  recipe:(id:string,pantry:string[])=>request<RecipeDetail>(`/api/recipes/${encodeURIComponent(id)}/availability`,{method:'POST',body:JSON.stringify({ingredientIds:pantry})}),
  graph:()=>request<GraphData>('/api/graph')
}
