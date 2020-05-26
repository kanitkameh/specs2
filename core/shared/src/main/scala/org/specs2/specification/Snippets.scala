package org.specs2
package specification

import scala.quoted._
import org.specs2.execute._
import specification.core._
import specification.create._

/**
 * Snippets of code can be extracted from interpolated specification strings.
 *
 * When you want to specify that a piece of code must be included in the specification output, you can use the `snippet`
 * method to execute a this code and use the text in the output. If you just want to output part of the code you need to
 * delimit it with some comments `// 8<-------` (with as many dashes as you want)
 *
 * Generally the last value of a snippet will be displayed separately but it is possible to avoid this by using the `mute`
 * method on a Snippet.
 *
 * It is also possible to check that the result value is equal to a specific value by using the `check[R : AsResult](f: T => R)` method.
 *
 */
trait Snippets extends org.specs2.execute.Snippets { outer: S2StringContextCreation with FragmentsFactory =>
  private val factory = outer.fragmentFactory

  implicit inline def snippetIsInterpolatedFragment[T](inline snippet: Snippet[T]): InterpolatedFragment =
    ${Snippets.createInterpolatedFragment('{snippet}, '{outer.fragmentFactory})}
}

object Snippets {

  def createInterpolatedFragment[T](snippetExpr: Expr[Snippet[T]], factoryExpr: Expr[FragmentFactory])(
    using qctx: QuoteContext, t: Type[T]): Expr[InterpolatedFragment] = {
    import qctx.tasty._
    '{
       new InterpolatedFragment {
         private val expression = ${Expr(rootPosition.sourceCode)}
         private val snippet: Snippet[$t] = ${snippetExpr}
         private val factory = ${factoryExpr}

         def append(fs: Fragments, text: String, start: Location, end: Location): Fragments =
           (fs append factory.text(text).setLocation(start)) append snippetFragments(snippet, end, expression)

         def snippetFragments(snippet: Snippet[$t], location: Location, expression: String): Fragments = {
           Fragments(
             Seq(factory.text(snippet.show(expression)).setLocation(location)) ++
               resultFragments(snippet, location) ++
               checkFragments(snippet, location):_*)
         }
         def resultFragments(snippet: Snippet[$t], location: Location) = {
           if (snippet.showResult.isEmpty)
             Seq()
           else
             Seq(factory.text("\n"+snippet.showResult).setLocation(location))
         }
         def checkFragments(snippet: Snippet[$t], location: Location) = {
           if (snippet.mustBeVerified)
             Seq(factory.step(snippet.verify.mapMessage("Snippet failure: "+_)).setLocation(location))
           else
             Seq()
         }
       }
    }
  }

}
