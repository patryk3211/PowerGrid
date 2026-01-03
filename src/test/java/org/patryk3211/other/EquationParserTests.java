/*
 * Copyright 2026 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.other;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.patryk3211.powergrid.electricity.info.customdisplay.Expression;

public class EquationParserTests {
    @Test
    public void constExprParse() {
        var expr1 = Expression.parse("5 + 1 - 3");
        Assertions.assertEquals(3, expr1.eval(0), "Expression evaluated to an incorrect value");

        var expr2 = Expression.parse("-5 + 1 - 3");
        Assertions.assertEquals(-7, expr2.eval(0), "Expression evaluated to an incorrect value");

        var expr3 = Expression.parse("-(5 + 1) - 3");
        Assertions.assertEquals(-9, expr3.eval(0), "Expression evaluated to an incorrect value");

        var expr4 = Expression.parse("5 - (5 + 1) - 3");
        Assertions.assertEquals(-4, expr4.eval(0), "Expression evaluated to an incorrect value");

        var expr5 = Expression.parse("5 * 2 + 1 / 4");
        Assertions.assertEquals(10.25, expr5.eval(0), "Expression evaluated to an incorrect value");

        var expr6 = Expression.parse("5 * (2 + 1) / 4");
        Assertions.assertEquals(15 / 4.0, expr6.eval(0), "Expression evaluated to an incorrect value");
    }

    @Test
    public void variableExprParse() {
        var expr1 = Expression.parse("5 + x - 3");
        Assertions.assertEquals(2, expr1.eval(0), "Expression evaluated to an incorrect value");
        Assertions.assertEquals(6, expr1.eval(4), "Expression evaluated to an incorrect value");

        var expr2 = Expression.parse("5 * (2 + 1) / x");
        Assertions.assertEquals(15 / 2.0, expr2.eval(2), "Expression evaluated to an incorrect value");
        Assertions.assertEquals(15 / 5.0, expr2.eval(5), "Expression evaluated to an incorrect value");

        var expr3 = Expression.parse("5 * (2 + x) / 4");
        Assertions.assertEquals(10 / 4.0, expr3.eval(0), "Expression evaluated to an incorrect value");
        Assertions.assertEquals(20 / 4.0, expr3.eval(2), "Expression evaluated to an incorrect value");

        var expr4 = Expression.parse("5 * ( - x)+1");
        Assertions.assertEquals(-4, expr4.eval(1), "Expression evaluated to an incorrect value");

        var expr5 = Expression.parse("-(x + 2) * 2");
        Assertions.assertEquals(-8, expr5.eval(2), "Expression evaluated to an incorrect value");
    }

    @Test
    void functionParsing() {
        var expr1 = Expression.parse("2 * sqrt(x) * x");
        Assertions.assertEquals(2 * Math.sqrt(5) * 5, expr1.eval(5), 1e-3, "Expression evaluated to an incorrect value");

        var expr2 = Expression.parse("abs(x)");
        Assertions.assertEquals(2, expr2.eval(2), 1e-3, "Expression evaluated to an incorrect value");
        Assertions.assertEquals(2, expr2.eval(-2), 1e-3, "Expression evaluated to an incorrect value");

        var expr3 = Expression.parse("-abs(x)");
        Assertions.assertEquals(-2, expr3.eval(2), 1e-3, "Expression evaluated to an incorrect value");
        Assertions.assertEquals(-2, expr3.eval(-2), 1e-3, "Expression evaluated to an incorrect value");
    }
}
