## What?

This is an incomplete and unofficial OPA Rego interpreter. It executes Rego policies using ANTLR4 visitor for the Rego grammar described in rego-grammar module. It is a Java replacement for OPA runtime which is implemented in Go.

## Why?

Because Java. This library also allows invoking user defined Java functions from Rego policy.

## Usage

See `MergedUTTest.java` for examples.

## Limitations / TODO
1. Many in-built functions are currently missing (see `ExprLibrary.java` for currently supported functions).
1. Parts of the grammar are left unimplemented (see test policies for coverage).
   1. The `some` keyword doesn't support multi-valued resolution (e.g. `some i; var[[1, i]]` won't work).
1. Policy is processed linearly. This example behaves differently in OPA playground: `package p a{b} b{true}`
1. No partial evaluation support.

## Links
* OPA: https://www.openpolicyagent.org/
* OPA Playground: https://play.openpolicyagent.org/

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.

