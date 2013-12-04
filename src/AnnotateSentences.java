
// Interface for implementing sentence annotations with wikipedia links and freebase IDs
// using different heuristics.
// For example: 
// 		- text = "Barack Obama met with Putin.";
//		- mentions = ("Barack Obama":(wikipedia/Barak_Obrama; freebaseid= 920))
//				   ("Putin":(wikipedia/Vladimir_Putin; freebaseid= 230))
// might do the annotation: "[Barack Obama]<#wikipedia/Barack_Obama;920#> met with [Putin]<#wikipedia/Putin;230#>."
public interface AnnotateSentences {

	public void annotateSentences(WikiLinkItem item);
	
	public int getTotalNumAnnotations();
}
