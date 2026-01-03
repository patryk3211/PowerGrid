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
package org.patryk3211.powergrid.electricity.info.customdisplay;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public interface Expression {

    float eval(float x);

    private static List<Integer> pass(int startIdx, String eqStr, Function<Character, Boolean> condition) {
        int depth = 0;
        var indices = new ArrayList<Integer>();
        for(int i = startIdx; i < eqStr.length(); ++i) {
            char c = eqStr.charAt(i);
            if(c == ' ')
                continue;
            if(c == '(') {
                ++depth;
                continue;
            } else if(c == ')') {
                if(--depth < 0)
                    throw new IllegalArgumentException("Equation format error");
                continue;
            } else {
                if(depth != 0)
                    continue;
            }
            // Negative number.
            if(c == '-' && i == 0)
                continue;
            if(condition.apply(c))
                indices.add(i);
        }
        if(indices.isEmpty())
            return null;
        return indices;
    }

    static Optional<Expression> tryParse(String eqStr) {
        try {
            return Optional.of(parse(eqStr));
        } catch(Exception e) {
            return Optional.empty();
        }
    }

    private static String subFunc(String in, int funLen) {
        return in.substring(funLen, in.length() - 1);
    }

    static Expression parse(String eqStr) {
        eqStr = eqStr.trim();
        if(eqStr.charAt(0) == '(' && eqStr.charAt(eqStr.length() - 1) == ')')
            eqStr = eqStr.substring(1, eqStr.length() - 1).trim();

        Expression eq = null;
        var indices = pass(0, eqStr, c -> c == '+' || c == '-');
        if(indices != null) {
            int prevIdx = 0;
            for(int i = 0; i < indices.size(); ++i) {
                Expression e1, e2;
                var index = indices.get(i);
                var nextIdx = i + 1 < indices.size() ? indices.get(i + 1) : eqStr.length();
                if(eq == null) {
                    e1 = parse(eqStr.substring(prevIdx, index));
                    e2 = parse(eqStr.substring(index + 1, nextIdx));
                } else {
                    e1 = eq;
                    e2 = parse(eqStr.substring(index + 1, nextIdx));
                }
                prevIdx = index;
                if(eqStr.charAt(index) == '+') {
                    eq = new Add(e1, e2);
                } else {
                    eq = new Subtract(e1, e2);
                }
            }
            return eq;
        }
        indices = pass(0, eqStr, c -> c == '*' || c == '/');
        if(indices != null) {
            int prevIdx = 0;
            for(int i = 0; i < indices.size(); ++i) {
                Expression e1, e2;
                var index = indices.get(i);
                var nextIdx = i + 1 < indices.size() ? indices.get(i + 1) : eqStr.length();
                if(eq == null) {
                    e1 = parse(eqStr.substring(prevIdx, index));
                    e2 = parse(eqStr.substring(index + 1, nextIdx));
                } else {
                    e1 = eq;
                    e2 = parse(eqStr.substring(index + 1, nextIdx));
                }
                prevIdx = index;
                if(eqStr.charAt(index) == '*') {
                    eq = new Multiply(e1, e2);
                } else {
                    eq = new Divide(e1, e2);
                }
            }
            return eq;
        }
        if(eqStr.charAt(0) == '-') {
            return new Negate(parse(eqStr.substring(1)));
        } else if(eqStr.equals("x")) {
            return new Variable();
        } else {
            for(var entry : UnaryExpressions.EXPRESSIONS.entrySet()) {
                if(eqStr.startsWith(entry.getKey() + "(")) {
                    return entry.getValue()
                            .apply(parse(subFunc(eqStr, entry.getKey().length() + 1)));
                }
            }
        }

        return new Value(Float.parseFloat(eqStr));
    }
}

class Value implements Expression {
    private final float v;

    public Value(float v) {
        this.v = v;
    }

    @Override
    public float eval(float x) {
        return v;
    }
}

class Variable implements Expression {
    @Override
    public float eval(float x) {
        return x;
    }
}

abstract class OneValueExpr implements Expression {
    final Expression e;

    public OneValueExpr(Expression e) {
        this.e = e;
    }
}

class Negate extends OneValueExpr {
    public Negate(Expression e) {
        super(e);
    }

    @Override
    public float eval(float x) {
        return -e.eval(x);
    }
}

class Sqrt extends OneValueExpr {
    public Sqrt(Expression e) {
        super(e);
    }

    @Override
    public float eval(float x) {
        return (float) Math.sqrt(x);
    }
}

class Abs extends OneValueExpr {
    public Abs(Expression e) {
        super(e);
    }

    @Override
    public float eval(float x) {
        return Math.abs(e.eval(x));
    }
}

class Floor extends OneValueExpr {
    public Floor(Expression e) {
        super(e);
    }

    @Override
    public float eval(float x) {
        return (float) Math.floor(e.eval(x));
    }
}

class Ceil extends OneValueExpr {
    public Ceil(Expression e) {
        super(e);
    }

    @Override
    public float eval(float x) {
        return (float) Math.ceil(e.eval(x));
    }
}

class Round extends OneValueExpr {
    public Round(Expression e) {
        super(e);
    }

    @Override
    public float eval(float x) {
        return Math.round(e.eval(x));
    }
}

abstract class TwoValueExpr implements Expression {
    final Expression e1;
    final Expression e2;

    public TwoValueExpr(Expression e1, Expression e2) {
        this.e1 = e1;
        this.e2 = e2;
    }
}

class Multiply extends TwoValueExpr {
    public Multiply(Expression e1, Expression e2) {
        super(e1, e2);
    }

    @Override
    public float eval(float x) {
        return e1.eval(x) * e2.eval(x);
    }
}

class Divide extends TwoValueExpr {
    public Divide(Expression e1, Expression e2) {
        super(e1, e2);
    }

    @Override
    public float eval(float x) {
        return e1.eval(x) / e2.eval(x);
    }
}

class Add extends TwoValueExpr {
    public Add(Expression e1, Expression e2) {
        super(e1, e2);
    }

    @Override
    public float eval(float x) {
        return e1.eval(x) + e2.eval(x);
    }
}

class Subtract extends TwoValueExpr {
    public Subtract(Expression e1, Expression e2) {
        super(e1, e2);
    }

    @Override
    public float eval(float x) {
        return e1.eval(x) - e2.eval(x);
    }
}
