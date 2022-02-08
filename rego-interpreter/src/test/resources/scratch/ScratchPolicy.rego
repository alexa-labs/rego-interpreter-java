# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

package ScratchPolicy

default hello = false

hello {
    m := input.message
    m == "world"
}
