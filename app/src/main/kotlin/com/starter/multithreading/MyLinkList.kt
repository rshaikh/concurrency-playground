package com.starter.multithreading

class MyLinkList {
    private var head: Node? = null
    private var tail: Node? = null
    var size: Int = 0
    fun add(item: Int) {
        if(head == null && tail == null) {
            head = Node(item)
            tail= head
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
        if(size > 0) {
            value = head?.value
            head = head?.next
            if(head == null) {
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
