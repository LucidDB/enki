package org.eigenbase.enki.prototype;

import java.util.*;

public class ManyToManyAssociation
{
    private long id;
    private String type;

    private List<Associable> left;
    private List<Associable> right;

    public ManyToManyAssociation()
    {
    }
    
    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }
    
    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
    
    public List<Associable> getLeft()
    {
        return left;
    }

    /**
     * @throws ClassCastException if any child is not a <tt>cls</tt>
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getLeft(Class<E> cls)
    {
        for(Object child: left) {
            cls.cast(child);
        }

        return (List<E>)left;
    }

    public void setLeft(List<Associable> children)
    {
        System.out.println("notify-special-case");
        this.left = new NotifyingArrayList<Associable>(children);
    }

    public <E extends Associable> void addLeft(E child)
    {
        if (left == null) {
            left = 
                new NotifyingArrayList<Associable>(new ArrayList<Associable>());
        }

        left.add(child);
    }

    public boolean removeLeft(Associable child)
    {
        if (left == null) {
            return false;
        }

        return left.remove(child);
    }

    public List<Associable> getRight()
    {
        return right;
    }

    /**
     * @throws ClassCastException if any child is not a <tt>cls</tt>
     */
    @SuppressWarnings("unchecked")
    public <E> List<E> getRight(Class<E> cls)
    {
        for(Object child: right) {
            cls.cast(child);
        }

        return (List<E>)right;
    }

    public void setRight(List<Associable> children)
    {
        System.out.println("notify-special-case");
        this.right = new NotifyingArrayList<Associable>(children);
    }

    public <E extends Associable> void addRight(E child)
    {
        if (right == null) {
            right= 
                new NotifyingArrayList<Associable>(new ArrayList<Associable>());
        }

        right.add(child);
    }

    public boolean removeRight(Associable child)
    {
        if (right == null) {
            return false;
        }

        return right.remove(child);
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder();

        b
            .append("One-To-Many@")
            .append(id)
            .append("([");
        boolean first = true;
        for(Object child: getLeft()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(child);
        }
        b.append("]->[");
        first = true;
        for(Object child: getRight()) {
            if (first) {
                first = false;
            } else {
                b.append(", ");
            }
            b.append(child);
        }
        b.append(']');
        return b.toString();
    }

}

// End ManyToManyAssociation.java
