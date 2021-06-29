package com.starter.multithreading

class MyLinkList {
    private var head: Node? = null
    private var tail: Node? = null
    var size: Int = 0
    fun add(item: Int) {
        if (isEmpty()) {
            val node = Node(item)
            head = node
            tail = node
        } else {
            val node = Node(item)
            tail?.next = node
            tail = node
        }
        size++
    }

    fun size(): Int {
        return size
    }

    fun remove(): Int? {
        var value: Int? = null
        if (!isEmpty()) {
            value = head?.value
            head = head?.next
            val isLastElementRemoved = head == null
            if (isLastElementRemoved) {
                tail = null
            }
            size--
        }
        return value
    }

    fun isEmpty(): Boolean {
        return head == null && tail == null
    }
}

class Node(val value: Int, var next: Node? = null) {

}
