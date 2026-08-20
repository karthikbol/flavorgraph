export type Option={id:string;name:string;category:string}
export type SearchRequest={ingredientIds:string[];applianceIds:string[];cuisineId:string;dietIds:string[];maxCookTimeMinutes:number|null}
export type Recipe={id:string;name:string;description:string;cookTimeMinutes:number;difficulty:string;cookingMethod:string;imageUrl:string;cuisine:string;diets:string[];appliances:string[];ingredientMatchPercentage:number;matchedIngredientCount:number;totalIngredientCount:number;missingIngredientCount:number;possibleWithSubstitutions:boolean}
export type Substitution={id:string;name:string;similarityScore:number;note:string;owned:boolean}
export type IngredientLine={id:string;name:string;category:string;quantity:number;unit:string;available:boolean;substitutions:Substitution[]}
export type RecipeDetail=Omit<Recipe,'ingredientMatchPercentage'|'matchedIngredientCount'|'totalIngredientCount'|'missingIngredientCount'|'possibleWithSubstitutions'> & {ingredients:IngredientLine[];instructions:string[]}
export type GraphData={nodes:{id:string;label:string;name:string;properties:Record<string,unknown>}[];edges:{source:string;target:string;type:string;properties:Record<string,unknown>}[]}
