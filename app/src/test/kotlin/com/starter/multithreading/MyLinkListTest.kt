package com.starter.multithreading

import org.amshove.kluent.`should equal`
import org.junit.jupiter.api.Test

class MyLinkListTest {
    @Test
    fun `it should add a element to the list`() {
        val list = MyLinkList()

        list.add(10)

        list.size() `should equal` 1
    }

    @Test
    fun `it should add two element to the list`() {
        val list = MyLinkList()

        list.add(10)
        list.add(20)

        list.size() `should equal` 2
    }

    @Test
    fun `it should remove the first element from the list`() {
        val list = MyLinkList()

        list.add(10)
        list.add(20)

        list.remove()!! `should equal` 10
    }

    @Test
    fun `it should remove the last element from list and mark it as empty`() {
        val list = MyLinkList()

        list.add(10)

        list.remove()!! `should equal` 10
        list.isEmpty() `should equal` true
    }

    @Test
    fun `it should remove the first two element and return correct size when we have 5 elements in list`() {
        val list = MyLinkList()

        list.add(10)
        list.add(5)
        list.add(8)
        list.add(19)
        list.add(30)

        list.size `should equal` 5
        list.remove()!! `should equal` 10
        list.size `should equal` 4
        list.remove()!! `should equal` 5
        list.size `should equal` 3
        list.isEmpty() `should equal` false

        list.remove()!! `should equal` 8
        list.size `should equal` 2

        list.remove()!! `should equal` 19
        list.size `should equal` 1

        list.remove()!! `should equal` 30
        list.size `should equal` 0
        list.isEmpty() `should equal` true
    }
}