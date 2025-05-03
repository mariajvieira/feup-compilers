package pt.up.fe.comp2025.optimization;

import org.specs.comp.ollir.Element;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.Operand;
import org.specs.comp.ollir.Operation;
import org.specs.comp.ollir.inst.*;

import java.util.*;

public class RegisterAllocator {

    public static Map<String, Integer> allocate(Method method, int r) {
        Map<String, Interval> intervals = computeIntervals(method);
        Map<String, Set<String>> graph = buildInterferenceGraph(intervals);

        int k = (r <= 0) ? intervals.size() : r;
        Map<String, Integer> coloring = colorGraph(graph, k);
        if (coloring == null && r > 0) {
            throw new RuntimeException(
                    "Not enough registers: need at least " + graph.size());
        }
        if (r == 0) {
            int used = Collections.max(coloring.values()) + 1;
            k = used;
        }

        int base = (method.isStaticMethod() ? 0 : 1) + method.getParams().size();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (var entry : coloring.entrySet()) {
            result.put(entry.getKey(), base + entry.getValue());
        }
        return result;
    }

    private static Map<String, Set<String>> buildInterferenceGraph(
            Map<String, Interval> intervals) {
        Map<String, Set<String>> graph = new LinkedHashMap<>();
        List<Map.Entry<String, Interval>> list = new ArrayList<>(intervals.entrySet());

        for (int i = 0; i < list.size(); i++) {
            var a = list.get(i);
            graph.putIfAbsent(a.getKey(), new HashSet<>());

            for (int j = i + 1; j < list.size(); j++) {
                var b = list.get(j);
                if (a.getValue().overlaps(b.getValue())) {
                    graph.get(a.getKey()).add(b.getKey());
                    graph.computeIfAbsent(b.getKey(), v -> new HashSet<>())
                            .add(a.getKey());
                }
            }
        }

        return graph;
    }

    private static Map<String, Integer> colorGraph(
            Map<String, Set<String>> graph, int k) {
        Map<String, Integer> color = new LinkedHashMap<>();
        var nodes = new ArrayList<>(graph.keySet());
        nodes.sort((x, y) -> graph.get(y).size() - graph.get(x).size());

        for (String n : nodes) {
            Set<Integer> forbidden = new HashSet<>();
            for (String nb : graph.get(n)) {
                if (color.containsKey(nb)) forbidden.add(color.get(nb));
            }
            int c = 0;
            while (forbidden.contains(c)) c++;
            if (c >= k) return null; // spill needed
            color.put(n, c);
        }

        return color;
    }

    private static class Interval {
        int start, end;

        Interval(int s, int e) {
            start = s;
            end = e;
        }

        boolean overlaps(Interval o) {
            return this.start <= o.end && o.start <= this.end;
        }
    }

    private static Map<String, Interval> computeIntervals(Method m) {
        Map<String, Interval> intervals = new LinkedHashMap<>();
        List<Instruction> code = m.getInstructions();

        for (int i = 0; i < code.size(); i++) {
            Instruction ins = code.get(i);
            final int idx = i;

            // def
            getDest(ins).ifPresent(dest -> {
                String var = getOperandName(dest);
                intervals.computeIfAbsent(var, v -> new Interval(idx, idx))
                        .start = idx;
            });

            // uses
            for (Operand use : getUses(ins)) {
                String var = getOperandName(use);
                intervals.computeIfAbsent(var, v -> new Interval(idx, idx))
                        .end = Math.max(intervals.get(var).end, idx);
            }
        }

        return intervals;
    }

    private static Optional<Operand> getDest(Instruction ins) {
        if (ins instanceof AssignInstruction ai) {
            Element e = ai.getDest();
            if (e instanceof Operand) return Optional.of((Operand) e);
        }
        if (ins instanceof ReturnInstruction ri && ri.hasReturnValue()) {
            Element e = ri.getOperand().orElse(null);
            if (e instanceof Operand) return Optional.of((Operand) e);
        }
        return Optional.empty();
    }

    private static List<Operand> getUses(Instruction ins) {
        List<Operand> uses = new ArrayList<>();

        if (ins instanceof AssignInstruction ai) {
            Instruction rhsIns = ai.getRhs();

            if (rhsIns instanceof org.specs.comp.ollir.inst.BinaryOpInstruction bi) {
                Element l = bi.getLeftOperand(), r = bi.getRightOperand();
                if (l instanceof Operand) uses.add((Operand) l);
                if (r instanceof Operand) uses.add((Operand) r);

            } else if (rhsIns instanceof org.specs.comp.ollir.inst.UnaryOpInstruction ui) {
                Element o = ui.getOperand();
                if (o instanceof Operand) uses.add((Operand) o);

            } else if (rhsIns instanceof CallInstruction ci) {
                for (Element e : ci.getOperands()) {
                    if (e instanceof Operand) uses.add((Operand) e);
                }
            }
            // else: no further uses in simple copy
        }
        else if (ins instanceof BinaryOpInstruction bi) {
            Element l = bi.getLeftOperand(), r = bi.getRightOperand();
            if (l instanceof Operand) uses.add((Operand) l);
            if (r instanceof Operand) uses.add((Operand) r);
        }
        else if (ins instanceof UnaryOpInstruction ui) {
            Element o = ui.getOperand();
            if (o instanceof Operand) uses.add((Operand) o);
        }
        else if (ins instanceof CallInstruction ci) {
            for (Element e : ci.getOperands()) {
                if (e instanceof Operand) uses.add((Operand) e);
            }
        }
        else if (ins instanceof ReturnInstruction ri && ri.hasReturnValue()) {
            ri.getOperand().ifPresent(o -> {
                if (o instanceof Operand) uses.add((Operand) o);
            });
        }

        return uses;
    }
    private static String getOperandName(Operand op) {
        try {
            return op.getName();
        } catch (NoSuchMethodError | AbstractMethodError e) {
            return op.toString();
        }
    }
}