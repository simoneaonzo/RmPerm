package it.unige.dibris.rmperm.loader;

import it.unige.dibris.rmperm.DexMethod;
import it.unige.dibris.rmperm.IOutput;
import org.jf.dexlib2.iface.reference.MethodReference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class PermissionToMethodsParser {

    private static final Pattern permPattern = Pattern.compile("^Permission:(android.permission.[A-Z_]*)$");
    private static final Pattern methodPattern = Pattern.compile(
            "<(?<defClass>(?:\\w|\\.)+): (?<retType>(?:\\w|\\.)+) (?<name>(?:\\w|\\.)+)\\((?<params>[^)]*)\\)>.*");

    private final IOutput out;

    public PermissionToMethodsParser(IOutput out) {
        this.out = out;
    }

    public Map<MethodReference, Set<String>> loadMapping(Set<String> permissionToRemove) throws IOException {
        final Map<MethodReference, Set<String>> result = new HashMap<>();
        InputStream resourceAsStream = this.getClass()
                                           .getResourceAsStream("/jellybean_publishedapimapping");
        BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream));
        String line;
        String permission = null;
        while ((line = reader.readLine()) != null) {
            Matcher permMatcher = permPattern.matcher(line);
            if (permMatcher.matches()) {
                permission = permMatcher.group(1);
            } else {
                assert permission != null;
                if (!permissionToRemove.contains(permission))
                    continue;
                Matcher methodMatcher = methodPattern.matcher(line);
                if (methodMatcher.matches()) {
                    String definingClass = DexMethod.fromJavaTypeToDalvikType(methodMatcher.group("defClass"));
                    String returnType = DexMethod.fromJavaTypeToDalvikType(methodMatcher.group("retType"));
                    String name = methodMatcher.group("name");
                    List<String> params = DexMethod.parseAndConvertIntoDalvikTypes(methodMatcher.group("params"));
                    DexMethod dm = new DexMethod(definingClass, name, params, returnType);
                    if (!result.containsKey(dm))
                        result.put(dm, new HashSet<>());
                    result.get(dm)
                          .add(permission);
                }
            }
        }
        out.printf(IOutput.Level.DEBUG,
                   String.format("Loaded %s API mappings for %d permissions\n",
                                 result.size(),
                                 permissionToRemove.size()));
        return result;
    }

}
