#!/usr/bin/env python3
from pathlib import Path
import re


path = Path("/usr/lib/python3/dist-packages/androguard/core/bytecodes/axml/__init__.py")
source = path.read_text(encoding="utf-8")
new = '''                if current_package.keyStrings:
                    # Next, we should have the resource key symbol table
                    self.buff.set_idx(current_package.header.start + current_package.keyStrings)
                    key_sp_header = ARSCHeader(self.buff, expected_type=RES_STRING_POOL_TYPE)
                    mKeyStrings = StringBlock(self.buff, key_sp_header)
                    key_sp_size = key_sp_header.size
                else:
                    key_sp_header = None
                    mKeyStrings = None
                    key_sp_size = 0

                self.packages[package_name].append(current_package)
                self.packages[package_name].append(mTableStrings)
                if mKeyStrings is not None:
                    self.packages[package_name].append(mKeyStrings)

                pc = PackageContext(current_package, self.stringpool_main, mTableStrings, mKeyStrings)
                log.debug("Constructed a PackageContext: %s", pc)

                next_idx = res_header.start + res_header.header_size + type_sp_header.size + key_sp_size
'''
pattern = re.compile(
    r'[ \t]+# Next, we should have the resource key symbol table\n'
    r'.*?'
    r'[ \t]+next_idx = res_header\.start \+ res_header\.header_size \+ '
    r'type_sp_header\.size \+ key_sp_header\.size\n',
    re.DOTALL,
)
if new in source:
    raise SystemExit("Androguard is already patched")
source, replacements = pattern.subn(new, source, count=1)
if replacements != 1:
    raise SystemExit("Expected Androguard resource parser block was not found")

reserved_field = '''        if self.res1 != 0:
            raise ResParserError("res1 must be zero!")
'''
if reserved_field not in source:
    raise SystemExit("Expected Androguard type-spec check was not found")
source = source.replace(reserved_field, "", 1)
path.write_text(source, encoding="utf-8")
